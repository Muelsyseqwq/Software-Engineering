-- Keep active cat archive status aligned with health status.
-- Healthy cats are available for interaction; non-healthy cats are resting.
-- DISABLED means the cat archive is stopped and should not be overwritten by health status.

UPDATE cat
SET status = 'AVAILABLE'
WHERE deleted = 0
  AND health_status = '健康'
  AND status <> 'DISABLED';

UPDATE cat
SET status = 'RESTING'
WHERE deleted = 0
  AND (health_status IS NULL OR health_status <> '健康')
  AND status <> 'DISABLED';
