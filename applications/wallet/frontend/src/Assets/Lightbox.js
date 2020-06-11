import { useEffect, useState } from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import AriaModal from 'react-aria-modal'

import { spacing, colors, constants } from '../Styles'

import CrossSvg from '../Icons/cross.svg'

import Button, { VARIANTS } from '../Button'
import SuspenseBoundary from '../SuspenseBoundary'
import AssetAsset from '../Asset/Asset'

/* istanbul ignore next */
const AssetsLightbox = ({ assets }) => {
  const {
    query: { projectId, id: selectedId },
  } = useRouter()

  const [isVisible, setIsVisible] = useState(false)

  const [cursor, setCursor] = useState(0)

  const i = assets.findIndex(({ id }) => id === selectedId)

  const index = i === -1 ? 0 : i

  const { id: assetId } = assets[index + cursor] || {}

  const keydownHandler = ({ code }) => {
    if (code === 'Space') {
      setIsVisible((currentIsVisible) => !currentIsVisible)

      setCursor(0)
    }

    if (code === 'ArrowLeft') {
      setCursor((currentCursor) => {
        if (index + currentCursor === 0) {
          return currentCursor
        }

        return currentCursor - 1
      })
    }

    if (code === 'ArrowRight') {
      setCursor((currentCursor) => {
        if (index + currentCursor === assets.length - 1) {
          return currentCursor
        }

        return currentCursor + 1
      })
    }
  }

  useEffect(() => {
    document.addEventListener('keydown', keydownHandler)

    return () => document.removeEventListener('keydown', keydownHandler)
  })

  if (projectId && assetId) {
    return (
      <AriaModal
        titleId="Asset"
        getApplicationNode={() => document.getElementById('__next')}
        underlayColor="rgba(0, 0, 0, 0.9)"
        onExit={() => setIsVisible(false)}
        dialogStyle={{ width: '100%' }}
        mounted={isVisible}
        focusDialog
        verticallyCenter
      >
        <div css={{ width: '100%' }}>
          <Button
            variant={VARIANTS.MENU_ITEM}
            onClick={() => setIsVisible(false)}
            css={{
              position: 'absolute',
              top: spacing.normal,
              left: spacing.normal,
              padding: spacing.base,
              color: colors.structure.steel,
              backgroundColor: colors.structure.mattGrey,
              borderRadius: constants.borderRadius.round,
              ':hover': {
                color: colors.structure.white,
              },
            }}
          >
            <CrossSvg width={20} />
          </Button>

          <div
            css={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              padding: spacing.spacious,
              '.ErrorBoundary > div': {
                backgroundColor: 'transparent',
                boxShadow: 'none',
              },
              '.Loading': {
                backgroundColor: 'transparent',
                boxShadow: 'none',
              },
            }}
          >
            <SuspenseBoundary>
              <AssetAsset
                key={assetId}
                projectId={projectId}
                assetId={assetId}
              />
            </SuspenseBoundary>
          </div>
        </div>
      </AriaModal>
    )
  }

  return null
}

AssetsLightbox.propTypes = {
  assets: PropTypes.arrayOf(PropTypes.object).isRequired,
}

export default AssetsLightbox
