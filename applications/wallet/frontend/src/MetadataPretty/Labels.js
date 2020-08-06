import { useRouter } from 'next/router'
import useSWR from 'swr'

import { constants, spacing, colors, typography } from '../Styles'

import MetadataPrettyLabelsContent from './LabelsContent'

const COLUMNS = ['bbox', 'model name/label', 'scope']

const MetadataPrettyLabels = () => {
  const {
    query: { projectId, assetId, id },
  } = useRouter()

  const {
    data: { results: models },
  } = useSWR(`/api/v1/projects/${projectId}/models/`)

  return (
    <div css={{ padding: spacing.normal, paddingBottom: spacing.comfy }}>
      <table
        css={{
          fontFamily: typography.family.mono,
          fontSize: typography.size.small,
          lineHeight: typography.height.small,
          color: colors.structure.white,
          width: '100%',
          borderSpacing: 0,
        }}
      >
        <thead>
          <tr>
            {COLUMNS.map((column) => {
              return (
                <th
                  key={column}
                  css={{
                    fontFamily: typography.family.condensed,
                    fontWeight: typography.weight.regular,
                    textTransform: 'uppercase',
                    color: colors.structure.steel,
                    paddingBottom: spacing.base,
                    paddingLeft: 0,
                    borderBottom: constants.borders.regular.smoke,
                    textAlign: 'left',
                    '&:nth-of-type(2)': {
                      width: '100%',
                    },
                    '&:last-of-type': {
                      textAlign: 'right',
                      whiteSpace: 'nowrap',
                      paddingRight: 0,
                    },
                  }}
                >
                  {column}
                </th>
              )
            })}
          </tr>
        </thead>

        <tbody>
          <MetadataPrettyLabelsContent
            projectId={projectId}
            assetId={id || assetId}
            models={models}
          />
        </tbody>
      </table>
    </div>
  )
}

export default MetadataPrettyLabels
