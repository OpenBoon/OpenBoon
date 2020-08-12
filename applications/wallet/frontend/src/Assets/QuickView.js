import { useEffect, useState } from 'react'
import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'

import { spacing, colors, constants, zIndex } from '../Styles'

import CrossSvg from '../Icons/cross.svg'

import Button, { VARIANTS } from '../Button'
import SuspenseBoundary from '../SuspenseBoundary'
import AssetAsset from '../Asset/Asset'
import { getQueryString } from '../Fetch/helpers'

const AssetsQuickView = ({ assets, columnCount }) => {
  const {
    query: { projectId, assetId, query },
  } = useRouter()

  const [isVisible, setIsVisible] = useState(false)

  const index = assets.findIndex(({ id }) => id === assetId)

  const { id: previousId } = index > 0 ? assets[index - 1] : {}
  const { id: nextId } =
    index > -1 && index < assets.length - 1 ? assets[index + 1] : {}
  const { id: previousRowId } =
    index > columnCount - 1 ? assets[index - columnCount] : {}
  const { id: nextRowId } =
    index > -1 && index < assets.length - columnCount
      ? assets[index + columnCount]
      : {}

  const keydownHandler = (event) => {
    const {
      code,
      target: { tagName },
    } = event

    /* istanbul ignore next */
    if (['INPUT', 'TEXTAREA'].includes(tagName)) return

    if (!assetId) return

    if (code === 'Escape') {
      setIsVisible(false)

      event.preventDefault()
    }

    if (code === 'Space') {
      setIsVisible((currentIsVisible) => !currentIsVisible)

      event.preventDefault()
    }

    if (code === 'ArrowLeft' && previousId) {
      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, assetId: previousId, query },
        },
        `/${projectId}/visualizer${getQueryString({
          assetId: previousId,
          query,
        })}`,
      )

      event.preventDefault()
    }

    if (code === 'ArrowRight' && nextId) {
      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, assetId: nextId, query },
        },
        `/${projectId}/visualizer${getQueryString({ assetId: nextId, query })}`,
      )

      event.preventDefault()
    }

    if (code === 'ArrowUp' && previousRowId) {
      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, assetId: previousRowId, query },
        },
        `/${projectId}/visualizer${getQueryString({
          assetId: previousRowId,
          query,
        })}`,
      )

      event.preventDefault()
    }

    if (code === 'ArrowDown' && nextRowId) {
      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, assetId: nextRowId, query },
        },
        `/${projectId}/visualizer${getQueryString({
          assetId: nextRowId,
          query,
        })}`,
      )

      event.preventDefault()
    }
  }

  useEffect(() => {
    document.addEventListener('keydown', keydownHandler)

    return () => document.removeEventListener('keydown', keydownHandler)
  })

  if (isVisible && projectId && assetId) {
    return (
      <div
        css={{
          position: 'absolute',
          top: 0,
          bottom: 0,
          left: 0,
          right: 0,
          zIndex: zIndex.layout.modal,
          backgroundColor: constants.overlay,
        }}
      >
        <Button
          aria-label="Close"
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
          <CrossSvg height={constants.icons.regular} />
        </Button>

        <div
          css={{
            width: '100%',
            height: '100%',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            padding: spacing.spacious,
          }}
        >
          <SuspenseBoundary isTransparent>
            <AssetAsset key={assetId} projectId={projectId} assetId={assetId} />
          </SuspenseBoundary>
        </div>
      </div>
    )
  }

  return null
}

AssetsQuickView.propTypes = {
  assets: PropTypes.arrayOf(PropTypes.object).isRequired,
  columnCount: PropTypes.number.isRequired,
}

export default AssetsQuickView
