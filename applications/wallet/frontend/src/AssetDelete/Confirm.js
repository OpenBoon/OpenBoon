import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'
import { useState } from 'react'
import { mutate, cache } from 'swr'

import { fetcher } from '../Fetch/helpers'
import { formatUrl } from '../Filters/helpers'

import { spacing, colors, typography } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS } from '../Button'

const AssetDeleteConfirm = ({ filename, setShowConfirmationDialogue }) => {
  const {
    query: { projectId, id: assetId, query },
  } = useRouter()

  const [hasError, setHasError] = useState(false)

  /* istanbul ignore next */
  const keysToUpdate = cache.keys().filter((key) => {
    return (
      !key.includes('err@') &&
      (key.includes('/searches/query') ||
        key.includes('/searches/aggregate/?filter='))
    )
  })
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
            setShowConfirmationDialogue(false)
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
          onClick={async () => {
            try {
              await fetcher(
                `/api/v1/projects/${projectId}/assets/${assetId}/`,
                {
                  method: 'DELETE',
                },
              )

              /* istanbul ignore next */
              keysToUpdate.forEach((key) =>
                mutate(
                  key,
                  fetch(key).then((res) => res.json()),
                ),
              )

              return Router.push(
                {
                  pathname: '/[projectId]/visualizer',
                  query: { query, id: '', action: 'delete-asset-success' },
                },
                `/${projectId}/visualizer${formatUrl({
                  id: '',
                  query,
                  action: 'delete-asset-success',
                })}`,
              )
            } catch (error) {
              return setHasError(true)
            }
          }}
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
  setShowConfirmationDialogue: PropTypes.func.isRequired,
}

export default AssetDeleteConfirm
