import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, spacing } from '../Styles'

import Assets from '../Assets'
import Metadata from '../Metadata'

const SIZE = 50

const VisualizerContent = () => {
  const {
    query: { projectId, page = 1 },
  } = useRouter()

  const parsedPage = parseInt(page, 10)
  const from = SIZE * (parsedPage - 1)

  const {
    data: { results: assets },
  } = useSWR(`/api/v1/projects/${projectId}/assets/?from=${from}&size=${SIZE}`)

  return (
    <div
      css={{
        height: '100%',
        backgroundColor: colors.structure.coal,
        marginLeft: -spacing.spacious,
        marginRight: -spacing.spacious,
        marginBottom: -spacing.spacious,
        marginTop: spacing.hairline,
        display: 'flex',
        flex: 1,
        flexDirection: 'column',
      }}
    >
      <div
        css={{
          display: 'flex',
          height: '100%',
          overflowY: 'hidden',
        }}
      >
        <Assets assets={assets} />
        <Metadata />
      </div>
    </div>
  )
}

export default VisualizerContent
