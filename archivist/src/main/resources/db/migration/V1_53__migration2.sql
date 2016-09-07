UPDATE migration SET str_name='archivist', str_path='classpath:/db/mappings/archivist/*.json';

INSERT INTO migration (int_type, str_name, str_path, int_version, time_applied)
  VALUES (0, 'analyst', 'classpath:/db/mappings/analyst/*.json', 0, 1454449960000);

