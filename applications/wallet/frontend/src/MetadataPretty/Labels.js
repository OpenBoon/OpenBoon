import { useRouter } from 'next/router'
import useSWR from 'swr'

import { constants, spacing, colors, typography } from '../Styles'

import MetadataPrettyLabelsContent from './LabelsContent'

const COLUMNS = ['bbox', 'dataset name/label', 'scope']

const MetadataPrettyLabels = () => {
  const {
    query: { projectId, assetId },
  } = useRouter()

  const {
    data: { results: datasets },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/all/`)

  return (
    <div css={{ paddingTop: spacing.normal, paddingBottom: spacing.comfy }}>
      <table>
        <thead>
          <tr>
            <td css={{ minWidth: spacing.normal }} />
            {COLUMNS.map((column) => {
              return (
                <th
                  key={column}
                  css={{
                    fontFamily: typography.family.condensed,
                    fontWeight: typography.weight.regular,
                    textTransform: 'uppercase',
                    color: colors.structure.steel,
                    padding: 0,
                    paddingBottom: spacing.base,
                    borderBottom: constants.borders.regular.smoke,
                    textAlign: 'left',
                    whiteSpace: 'nowrap',
                    '&:nth-of-type(2)': {
                      width: '100%',
                    },
                    '&:nth-of-type(3)': {
                      textAlign: 'right',
                    },
                  }}
                >
                  {column}
                </th>
              )
            })}
            <td />
            <td css={{ minWidth: spacing.base }} />
          </tr>
        </thead>

        <tbody>
          <MetadataPrettyLabelsContent
            projectId={projectId}
            assetId={assetId}
            datasets={datasets}
          />
        </tbody>
      </table>
    </div>
  )
}

export default MetadataPrettyLabels
