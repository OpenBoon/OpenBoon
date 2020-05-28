import PropTypes from 'prop-types'
import { useState } from 'react'

import { spacing, colors, typography } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS } from '../Button'

import { onDelete } from './helpers'

const AssetDeleteConfirm = ({ query: q, filename, setShowDialogue }) => {
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const { projectId, id: assetId, query } = q

  return (
    <div css={{ padding: spacing.normal }}>
      {error && (
        <div css={{ paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.ERROR}>{error}</FlashMessage>
        </div>
      )}
      <div
        css={{
          fontWeight: typography.weight.bold,
          paddingBottom: spacing.base,
        }}
      >
        {`Are you sure you want to delete "${filename}"?`}
      </div>
      <div
        css={{
          color: colors.signal.warning.base,
          paddingBottom: spacing.normal,
        }}
      >
        Deleting this asset is permanent and cannot be undone.
      </div>

      <div css={{ display: 'flex' }}>
        <Button
          variant={VARIANTS.SECONDARY}
          onClick={() => {
            setShowDialogue(false)
            setError('')
          }}
          style={{ flex: 1 }}
        >
          Cancel
        </Button>

        <div css={{ width: spacing.base, minWidth: spacing.base }} />

        <Button
          aria-label="Confirm Delete Asset"
          variant={VARIANTS.WARNING}
          onClick={() =>
            onDelete({
              projectId,
              assetId,
              query,
              setIsLoading,
              setError,
            })
          }
          style={{ flex: 1 }}
        >
          {isLoading ? 'Deleting...' : 'Delete Asset'}
        </Button>
      </div>
    </div>
  )
}

AssetDeleteConfirm.propTypes = {
  query: PropTypes.shape({
    projectId: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
  }).isRequired,
  filename: PropTypes.string.isRequired,
  setShowDialogue: PropTypes.func.isRequired,
}

export default AssetDeleteConfirm
