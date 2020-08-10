import { useRouter } from 'next/router'

import { colors, spacing } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'

import MetadataContent from './Content'

const Metadata = () => {
  const {
    query: { projectId, assetId },
  } = useRouter()

  return assetId ? (
    <SuspenseBoundary key={assetId}>
      <MetadataContent projectId={projectId} assetId={assetId} />
    </SuspenseBoundary>
  ) : (
    <div css={{ padding: spacing.normal, color: colors.structure.white }}>
      Select an asset to view its metadata.
    </div>
  )
}

export default Metadata
