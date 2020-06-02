import useSWR from 'swr'
import { useRouter } from 'next/router'

import { colors, spacing, typography } from '../Styles'

import { cleanup } from '../Filters/helpers'

const AssetsCount = () => {
  const {
    query: { projectId, query },
  } = useRouter()

  const q = cleanup({ query })

  const {
    data: { count },
  } = useSWR(`/api/v1/projects/${projectId}/searches/query/?query=${q}`)

  return (
    <div
      css={{
        padding: spacing.base,
        alignItems: 'center',
        fontFamily: 'Roboto Condensed',
        fontSize: typography.size.regular,
        lineHeight: typography.height.regular,
        backgroundColor: colors.structure.lead,
        color: colors.structure.steel,
      }}
    >
      {count} Assets
    </div>
  )
}

export default AssetsCount
