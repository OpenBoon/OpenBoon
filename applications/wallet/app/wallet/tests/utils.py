from rest_framework import status as drf_status


def check_response(response, status=drf_status.HTTP_200_OK):
    """Test helper to verify the response status code and return the json content."""
    assert response.status_code == status
    if status == drf_status.HTTP_204_NO_CONTENT:
        return None
    return response.json()
