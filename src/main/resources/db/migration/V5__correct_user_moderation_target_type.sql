UPDATE moderation_actions
SET target_type = 'USER'
WHERE target_type = 'POST'
  AND action_type IN ('RESTRICT_USER', 'ACTIVATE_USER');
