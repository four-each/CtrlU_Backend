INSERT INTO healthy (name)
SELECT val FROM (
                    SELECT '걷기' AS val
                    UNION ALL
                    SELECT '러닝'
                    UNION ALL
                    SELECT '자전거'
                ) AS temp
WHERE NOT EXISTS (SELECT 1 FROM healthy);