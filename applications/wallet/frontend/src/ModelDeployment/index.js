import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, spacing } from '../Styles'

import SectionTitle from '../SectionTitle'
import ItemList from '../Item/List'

const ModelDeployment = () => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const { data: model } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/`,
  )

  return (
    <div css={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
      <SectionTitle>Model REST API</SectionTitle>

      <div
        css={{
          color: colors.structure.zinc,
          paddingTop: spacing.normal,
          paddingBottom: spacing.normal,
        }}
      >
        This model is deployed as an easy to use REST API for integration into
        software projects.{' '}
        <a
          css={{ color: colors.key.two }}
          target="_blank"
          rel="noopener noreferrer"
          href="https://docs.boonai.app/boonsdk/solutions"
        >
          See Documentation
        </a>
      </div>

      <ItemList
        attributes={[
          [
            'Model Endpoint',
            `https://api.boonai.app/ap1/v1/analyze-file?modules=${model.name}`,
            'URL',
          ],
        ]}
      />
    </div>
  )
}

export default ModelDeployment
