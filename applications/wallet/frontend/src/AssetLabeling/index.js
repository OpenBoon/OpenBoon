import { useRouter } from 'next/router'

import { colors, spacing, typography } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'

import AssetLabelingContent from './Content'

const AssetLabeling = () => {
  const {
    query: { projectId, assetId },
  } = useRouter()

  return assetId ? (
    <SuspenseBoundary>
      <AssetLabelingContent projectId={projectId} assetId={assetId} />
    </SuspenseBoundary>
  ) : (
    <div
      css={{
        padding: spacing.normal,
        color: colors.structure.white,
        fontStyle: typography.style.italic,
      }}
    >
      Select an asset to add labels.
    </div>
  )
}

export default AssetLabeling
