package com.nekocafe.recommend.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekocafe.recommend.dto.RecommendationHighlight;
import com.nekocafe.recommend.dto.RecommendationStoreItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationReasonGenerator {

    private static final Logger log = LoggerFactory.getLogger(RecommendationReasonGenerator.class);
    private static final String SYSTEM_PROMPT = """
        你是 NekoCafe 猫咖系统的智能推荐文案助手，目标是写出像真人店员一样自然、具体、有场景感的推荐理由。
        只能使用输入中明确提供的信息，不要编造不存在的菜品、猫咪、活动、距离、优惠或用户信息。
        每家店生成 2 条中文理由，每条 35 到 75 个中文字符,三个店的推荐理由格式不要相同。
        每家店至少 1 条理由必须包含门店名、商圈或地址场景；至少 1 条理由必须引用具体菜品名、猫咪名或活动名。
        如果有 distance 字段，优先结合距离写；如果没有 distance，不要提远近。
        避免泛泛表达，例如“菜品丰富”“适合点单搭配”“体验更完整”“值得探索”“命中你的兴趣”。
        不要提及内部推荐分、用户 ID、系统实现、模型名称或隐私信息。
        输出必须是合法 JSON 对象，首字符必须是 {，不要使用 Markdown，不要添加解释或前后缀文字。
        JSON 格式必须严格为：{"items":[{"storeId":数字,"reasons":["理由1","理由2"]}]}。
        """;

    private final AiReasonProperties properties;
    private final OpenAiCompatibleReasonClient client;
    private final ObjectMapper objectMapper;
    private volatile String lastStatus = "NOT_CALLED";
    private volatile String lastErrorType = "";
    private volatile String lastErrorMessage = "";

    public RecommendationReasonGenerator(
        AiReasonProperties properties,
        OpenAiCompatibleReasonClient client,
        ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public List<RecommendationStoreItem> enhanceReasons(List<RecommendationStoreItem> items) {
        if (items == null || items.isEmpty()) {
            lastStatus = "SKIPPED_EMPTY_ITEMS";
            lastErrorType = "";
            lastErrorMessage = "";
            return items;
        }
        if (!properties.available()) {
            lastStatus = "SKIPPED_NOT_AVAILABLE";
            lastErrorType = "";
            lastErrorMessage = "";
            return items;
        }
        try {
            String payload = objectMapper.writeValueAsString(new AiReasonRequest(
                "请为这些已经排序好的猫咖推荐结果重写更自然的推荐理由。templateReasons 只用于理解推荐信号，不能照抄。",
                items.stream().map(this::toPromptItem).toList()
            ));
            String content = client.chat(SYSTEM_PROMPT, payload);
            AiReasonResponse response = objectMapper.readValue(cleanJson(content), AiReasonResponse.class);
            Map<Long, List<String>> reasonsByStore = response.items() == null
                ? Map.of()
                : response.items().stream()
                    .filter(item -> item.storeId() != null && item.reasons() != null && !item.reasons().isEmpty())
                    .collect(Collectors.toMap(
                        AiReasonItem::storeId,
                        item -> sanitizeReasons(item.reasons()),
                        (left, right) -> left
                    ));
            if (reasonsByStore.isEmpty()) {
                lastStatus = "FALLBACK_EMPTY_AI_REASONS";
                lastErrorType = "";
                lastErrorMessage = "";
                return items;
            }
            lastStatus = "SUCCESS";
            lastErrorType = "";
            lastErrorMessage = "";
            return items.stream()
                .map(item -> replaceReasons(item, reasonsByStore.get(item.storeId())))
                .toList();
        } catch (Exception ex) {
            lastStatus = "FALLBACK_EXCEPTION";
            lastErrorType = ex.getClass().getSimpleName();
            lastErrorMessage = sanitizeErrorMessage(ex.getMessage());
            log.warn("AI recommendation reason generation failed; falling back to template reasons: {}", ex.getClass().getSimpleName());
            return items;
        }
    }

    public String lastStatus() {
        return lastStatus;
    }

    public String lastErrorType() {
        return lastErrorType;
    }

    public String lastErrorMessage() {
        return lastErrorMessage;
    }

    private RecommendationStoreItem replaceReasons(RecommendationStoreItem item, List<String> aiReasons) {
        if (aiReasons == null || aiReasons.isEmpty()) {
            return item;
        }
        return new RecommendationStoreItem(
            item.rank(),
            item.storeId(),
            item.storeName(),
            item.city(),
            item.businessArea(),
            item.address(),
            item.status(),
            item.distanceKm(),
            item.score(),
            item.tags(),
            aiReasons,
            item.dishHighlights(),
            item.catHighlights(),
            item.activityHighlights(),
            item.primaryActionText()
        );
    }

    private PromptStoreItem toPromptItem(RecommendationStoreItem item) {
        return new PromptStoreItem(
            item.storeId(),
            item.storeName(),
            item.businessArea(),
            item.distanceKm() == null ? null : item.distanceKm() + "km",
            item.tags(),
            names(item.dishHighlights()),
            names(item.catHighlights()),
            names(item.activityHighlights()),
            item.reasons()
        );
    }

    private List<String> names(List<RecommendationHighlight> highlights) {
        if (highlights == null) {
            return List.of();
        }
        return highlights.stream()
            .map(RecommendationHighlight::name)
            .filter(Objects::nonNull)
            .filter(name -> !name.isBlank())
            .limit(4)
            .toList();
    }

    private String sanitizeErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String sanitized = message
            .replaceAll("(?i)Bearer\\s+[A-Za-z0-9._\\-]+", "Bearer ***")
            .replaceAll("sk-[A-Za-z0-9._\\-]+", "sk-***");
        return sanitized.length() > 160 ? sanitized.substring(0, 160) : sanitized;
    }

    private List<String> sanitizeReasons(List<String> reasons) {
        return reasons.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(reason -> !reason.isBlank())
            .map(reason -> reason.length() > 120 ? reason.substring(0, 120) : reason)
            .distinct()
            .limit(3)
            .toList();
    }

    private String cleanJson(String content) throws JsonProcessingException {
        String value = content == null ? "" : content.trim();
        if (value.startsWith("```")) {
            int firstNewLine = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstNewLine >= 0 && lastFence > firstNewLine) {
                value = value.substring(firstNewLine + 1, lastFence).trim();
            }
        }
        if (!value.startsWith("{")) {
            int start = value.indexOf('{');
            int end = value.lastIndexOf('}');
            if (start >= 0 && end > start) {
                value = value.substring(start, end + 1);
            }
        }
        if (value.isBlank()) {
            throw new JsonProcessingException("AI response is blank") {};
        }
        return value;
    }

    private record AiReasonRequest(String task, List<PromptStoreItem> stores) {
    }

    private record PromptStoreItem(
        Long storeId,
        String storeName,
        String businessArea,
        String distance,
        List<String> tags,
        List<String> dishes,
        List<String> cats,
        List<String> activities,
        List<String> templateReasons
    ) {
    }

    private record AiReasonResponse(List<AiReasonItem> items) {
    }

    private record AiReasonItem(Long storeId, List<String> reasons) {
    }
}
