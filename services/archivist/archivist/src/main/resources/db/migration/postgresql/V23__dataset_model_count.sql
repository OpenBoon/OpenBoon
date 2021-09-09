update dataset as d0 set int_model_count = (
    select count(*) as int_model_count from dataset d
        inner join model m
            on (d.pk_dataset = m.pk_dataset)
    where d.pk_dataset = d0.pk_dataset
    group by (d.pk_dataset)
);