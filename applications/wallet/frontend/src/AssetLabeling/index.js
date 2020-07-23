import { useRouter } from 'next/router'

import { colors, spacing } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'

import AssetLabelingContent from './Content'

const AssetLabeling = () => {
  const {
    query: { projectId, id: assetId },
  } = useRouter()

  return assetId ? (
    <SuspenseBoundary key={assetId}>
      <AssetLabelingContent projectId={projectId} />
    </SuspenseBoundary>
  ) : (
    <div css={{ padding: spacing.normal, color: colors.structure.white }}>
      Select an asset to add labels.
    </div>
  )
}

export default AssetLabeling
