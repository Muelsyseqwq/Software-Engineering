-- Add handler_id to food_order to track which staff member started preparing the order.

ALTER TABLE food_order
  ADD COLUMN handler_id BIGINT NULL AFTER refund_status,
  ADD KEY idx_order_handler (handler_id);
