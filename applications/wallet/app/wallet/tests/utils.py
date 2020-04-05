from rest_framework import status


def check_response(response, status=status.HTTP_200_OK):
    """Test helper to verify the response status code and return the json content."""
    assert response.status_code == status
    return response.json()
