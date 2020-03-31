from rest_framework import serializers

from agreements.models import Agreement


class AgreementSerializer(serializers.ModelSerializer):
    class Meta:
        model = Agreement
        fields = ('id', 'user', 'policies_date', 'ip_address', 'created_date', 'modified_date')

    def to_representation(self, instance):
        self.fields['user'] = serializers.HyperlinkedRelatedField(view_name='user-detail',
                                                                  read_only=True)
        return super(AgreementSerializer, self).to_representation(instance)
