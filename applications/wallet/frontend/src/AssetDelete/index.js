import { useRouter } from 'next/router'
import { useState } from 'react'

import SuspenseBoundary from '../SuspenseBoundary'
import AssetDeleteContent from './Content'
import AssetDeleteConfirm from './Confirm'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'

import { spacing } from '../Styles'

const AssetDelete = () => {
  const {
    query: { id: assetId, action },
  } = useRouter()

  const [showDialogue, setShowDialogue] = useState(false)

  if (!assetId && action === 'delete-asset-success') {
    return (
      <div css={{ padding: spacing.normal }}>
        <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
          1 asset deleted.
        </FlashMessage>
      </div>
    )
  }

  if (!assetId) {
    return (
      <div css={{ padding: spacing.normal }}>Select an asset to delete.</div>
    )
  }

  if (assetId && showDialogue) {
    return (
      <SuspenseBoundary>
        <AssetDeleteConfirm setShowDialogue={setShowDialogue} />
      </SuspenseBoundary>
    )
  }

  return <AssetDeleteContent key={assetId} setShowDialogue={setShowDialogue} />
}

export default AssetDelete
