import { useRouter } from 'next/router'

import SuspenseBoundary from '../SuspenseBoundary'
import AssetDeleteContent from './Content'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'

import { spacing } from '../Styles'

const AssetDelete = () => {
  const {
    query: { id: assetId, action },
  } = useRouter()

  if (!assetId) {
    return action === 'delete-asset-success' ? (
      <div css={{ padding: spacing.normal }}>
        <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
          1 asset deleted.
        </FlashMessage>
      </div>
    ) : (
      <div css={{ padding: spacing.normal }}>Select an asset to delete.</div>
    )
  }

  return (
    <SuspenseBoundary>
      <AssetDeleteContent />
    </SuspenseBoundary>
  )
}

export default AssetDelete
