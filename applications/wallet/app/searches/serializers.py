from rest_framework import serializers

from searches.models import Search


class SearchSerializer(serializers.ModelSerializer):
    """Serializer to return a Saved Search"""

    class Meta:
        model = Search
        fields = ('id', 'project', 'name', 'search', 'created_date', 'modified_date',
                  'created_by')

    def to_representation(self, instance):
        self.fields['created_by'] = serializers.HyperlinkedRelatedField(view_name='user-detail',
                                                                        read_only=True)
        self.fields['project'] = serializers.HyperlinkedRelatedField(view_name='project-detail',
                                                                     read_only=True)
        return super(SearchSerializer, self).to_representation(instance)
