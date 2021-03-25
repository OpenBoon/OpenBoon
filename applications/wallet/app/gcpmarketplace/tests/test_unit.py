from gcpmarketplace.utils import sum_ml_usage


def test_sum_usage():
    project_usage = {'1': {'tier_1_image_count': 300,
                           'tier_1_video_hours': 1,
                           'tier_2_image_count': 100,
                           'tier_2_video_hours': 1},
                     '2': {'tier_1_image_count': 100,
                           'tier_1_video_hours': 7,
                           'tier_2_image_count': 100,
                           'tier_2_video_hours': 1}}
    assert sum_ml_usage(project_usage) == {'tier_1_image_count': 400,
                                           'tier_1_video_hours': 8,
                                           'tier_2_image_count': 200,
                                           'tier_2_video_hours': 2}
