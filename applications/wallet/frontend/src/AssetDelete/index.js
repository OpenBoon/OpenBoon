import { useRouter } from 'next/router'
import { useState } from 'react'

import { spacing, typography } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'

import AssetDeleteContent from './Content'

const AssetDelete = () => {
  const {
    query: { projectId, assetId, action },
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
      <div
        css={{ padding: spacing.normal, fontStyle: typography.style.italic }}
      >
        Select an asset to delete.
      </div>
    )
  }

  return (
    <SuspenseBoundary>
      <AssetDeleteContent
        projectId={projectId}
        assetId={assetId}
        showDialogue={showDialogue}
        setShowDialogue={setShowDialogue}
      />
    </SuspenseBoundary>
  )
}

export default AssetDelete
