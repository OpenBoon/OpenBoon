import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, spacing } from '../Styles'

import Assets from '../Assets'

import VisualizerInfobar from './Infobar'
import VisualizerMetadata from './Metadata'

const VisualizerContent = () => {
  const {
    query: { projectId, page = 1 },
  } = useRouter()

  const {
    data: { results: assets, count },
  } = useSWR(`/api/v1/projects/${projectId}/assets/?page=${page}`)

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
      }}>
      <VisualizerInfobar currentPage={page} totalCount={count} />
      <div
        css={{
          display: 'flex',
          height: '100%',
          overflowY: 'hidden',
        }}>
        <Assets assets={assets} />
        <VisualizerMetadata />
      </div>
    </div>
  )
}

export default VisualizerContent
