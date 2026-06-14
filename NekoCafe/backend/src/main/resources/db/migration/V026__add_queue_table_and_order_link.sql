ALTER TABLE waiting_queue_ticket
    ADD COLUMN table_id BIGINT NULL COMMENT '入座桌位ID' AFTER party_size;

ALTER TABLE food_order
    ADD COLUMN queue_ticket_id BIGINT NULL COMMENT '关联排队票ID' AFTER reservation_id;

CREATE INDEX idx_waiting_queue_ticket_table ON waiting_queue_ticket(table_id);
CREATE INDEX idx_food_order_queue_ticket ON food_order(queue_ticket_id);
