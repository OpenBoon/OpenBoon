import { useRouter } from 'next/router'

import { colors, spacing, typography } from '../Styles'

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
    <div
      css={{
        padding: spacing.normal,
        color: colors.structure.white,
        fontStyle: typography.style.italic,
      }}
    >
      Select an asset to view its metadata.
    </div>
  )
}

export default Metadata
