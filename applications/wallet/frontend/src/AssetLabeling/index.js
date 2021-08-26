import { useState } from 'react'
import { useRouter } from 'next/router'

import { colors, spacing, typography } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import BulkAssetLabeling from '../BulkAssetLabeling'

import AssetLabelingContent from './Content'

const AssetLabeling = () => {
  const [isBulkLabeling, setIsBulkLabeling] = useState(false)

  const {
    query: { projectId, assetId, query },
  } = useRouter()

  if (assetId) {
    return (
      <SuspenseBoundary>
        <AssetLabelingContent
          projectId={projectId}
          assetId={assetId}
          setIsBulkLabeling={setIsBulkLabeling}
        />
      </SuspenseBoundary>
    )
  }

  if (isBulkLabeling) {
    return (
      <BulkAssetLabeling
        projectId={projectId}
        query={query}
        setIsBulkLabeling={setIsBulkLabeling}
      />
    )
  }

  return (
    <div
      css={{
        padding: spacing.normal,
        color: colors.structure.white,
        fontStyle: typography.style.italic,
      }}
    >
      Select an asset to the left to label a single asset or click the button
      below to label all assets in the current search.
      <div css={{ height: spacing.normal }} />
      <Button
        variant={BUTTON_VARIANTS.PRIMARY}
        onClick={() => {
          setIsBulkLabeling(true)
        }}
      >
        Label All Assets in Search
      </Button>
    </div>
  )
}

export default AssetLabeling
