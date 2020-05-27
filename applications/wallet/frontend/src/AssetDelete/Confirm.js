import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import { useState } from 'react'

import { spacing, colors, typography } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS } from '../Button'

import { onDelete } from './helpers'

const AssetDeleteConfirm = ({ filename, dispatch }) => {
  const {
    query: { projectId, id: assetId, query },
  } = useRouter()

  const [hasError, setHasError] = useState(false)

  return (
    <div css={{ padding: spacing.normal }}>
      {hasError && (
        <div css={{ paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.ERROR}>
            There was an error. Please try again.
          </FlashMessage>
        </div>
      )}
      <div
        css={{
          fontWeight: typography.weight.bold,
          paddingBottom: spacing.base,
        }}
      >
        Are you sure you want to delete {filename}
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
          aria-label="Cancel"
          variant={VARIANTS.SECONDARY}
          onClick={() => {
            dispatch(false)
            setHasError(false)
          }}
          style={{
            flex: 1,
            display: 'flex',
            flexFlow: 'nowrap',
            alignItems: 'end',
            svg: { marginRight: spacing.base },
          }}
        >
          Cancel
        </Button>

        <div css={{ width: spacing.base, minWidth: spacing.base }} />

        <Button
          aria-label="Confirm Delete Asset"
          variant={VARIANTS.WARNING}
          onClick={() =>
            onDelete({ projectId, assetId, query, dispatch: setHasError })
          }
          style={{
            flex: 1,
            display: 'flex',
            flexFlow: 'nowrap',
            alignItems: 'end',
            svg: { marginRight: spacing.base },
          }}
        >
          Delete Asset
        </Button>
      </div>
    </div>
  )
}

AssetDeleteConfirm.propTypes = {
  filename: PropTypes.string.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default AssetDeleteConfirm
