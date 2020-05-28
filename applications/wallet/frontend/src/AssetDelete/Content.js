import { useRouter } from 'next/router'
import { useState } from 'react'
import useSWR from 'swr'

import { spacing, colors, constants, typography } from '../Styles'

import Button, { VARIANTS } from '../Button'
import AssetDeleteConfirm from './Confirm'

const RADIO_BUTTON_SIZE = 16
const RADIO_BUTTION_FILL_SIZE = 8

const AssetDeleteContent = () => {
  const {
    query: { projectId, id: assetId },
  } = useRouter()

  const {
    data: {
      metadata: {
        source: { filename },
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  const [showDialogue, setShowDialogue] = useState(false)

  if (showDialogue) {
    return (
      <AssetDeleteConfirm
        filename={filename}
        setShowDialogue={setShowDialogue}
      />
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
            setShowDialogue(true)
          }}
        >
          Delete Asset
        </Button>
      </form>
    </div>
  )
}

export default AssetDeleteContent
