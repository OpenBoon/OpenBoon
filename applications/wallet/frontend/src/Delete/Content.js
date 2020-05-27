import Router, { useRouter } from 'next/router'
import { useState } from 'react'
import useSWR, { mutate, cache } from 'swr'

import { fetcher } from '../Fetch/helpers'

import { spacing, colors, constants, typography } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS } from '../Button'
import { formatUrl } from '../Filters/helpers'

const RADIO_BUTTON_SIZE = 16
const RADIO_BUTTION_FILL_SIZE = 8

const DeleteContent = () => {
  const {
    query: { projectId, id: assetId, query },
  } = useRouter()

  const {
    data: {
      metadata: {
        source: { filename },
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  /* istanbul ignore next */
  const keysToUpdate = cache.keys().filter((key) => {
    return (
      !key.includes('err@') &&
      (key.includes('/searches/query') ||
        key.includes('/searches/aggregate/?filter='))
    )
  })

  const [showConfirmationDialogue, setShowConfirmationDialogue] = useState(
    false,
  )
  const [hasError, setHasError] = useState(false)

  if (showConfirmationDialogue) {
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

  return (
    <div css={{ padding: spacing.normal }}>
      <form
        action=""
        method="post"
        onSubmit={(event) => {
          event.preventDefault()
        }}
      >
        <div
          css={{ display: 'flex', position: 'relative', alignItems: 'center' }}
        >
          <input
            type="radio"
            id="deleteSelected"
            value="deleteSelected"
            checked
            css={{
              margin: 0,
              padding: 0,
              WebkitAppearance: 'none',
              borderRadius: RADIO_BUTTON_SIZE,
              width: RADIO_BUTTON_SIZE,
              height: RADIO_BUTTON_SIZE,
              border: constants.borders.inputHover,
            }}
          />
          <div
            css={{
              position: 'absolute',
              top: 6,
              left: 4,
              width: RADIO_BUTTION_FILL_SIZE,
              height: RADIO_BUTTION_FILL_SIZE,
              transition: 'all .3s ease',
              opacity: 100,
              backgroundColor: colors.key.one,
              borderRadius: RADIO_BUTTON_SIZE,
            }}
          />
          <label
            htmlFor="deleteSelected"
            css={{
              paddingLeft: spacing.base,
              fontWeight: typography.weight.bold,
            }}
          >
            Delete Selected: 1
          </label>
        </div>
        <div css={{ paddingLeft: spacing.comfy }}>
          Delete the selected asset
        </div>
        <div css={{ height: spacing.normal }} />

        <Button
          aria-label="Delete Asset"
          variant={VARIANTS.PRIMARY_SMALL}
          style={{ width: 'fit-content' }}
          onClick={() => {
            setShowConfirmationDialogue(true)
          }}
        >
          Delete Asset
        </Button>
      </form>
    </div>
  )
}

export default DeleteContent
