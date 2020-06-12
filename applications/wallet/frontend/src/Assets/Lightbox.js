import { useEffect, useState } from 'react'
import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'

import { spacing, colors, constants } from '../Styles'

import CrossSvg from '../Icons/cross.svg'

import Button, { VARIANTS } from '../Button'
import SuspenseBoundary from '../SuspenseBoundary'
import AssetAsset from '../Asset/Asset'
import { formatUrl } from '../Filters/helpers'

const AssetsLightbox = ({ assets, columnCount }) => {
  const {
    query: { projectId, id: selectedId, query },
  } = useRouter()

  const [isVisible, setIsVisible] = useState(false)

  const index = assets.findIndex(({ id }) => id === selectedId)

  const { id: previousId } = index > 0 ? assets[index - 1] : {}
  const { id: nextId } = index < assets.length - 1 ? assets[index + 1] : {}
  const { id: previousRowId } =
    index > columnCount - 1 ? assets[index - columnCount] : {}
  const { id: nextRowId } =
    index < assets.length - columnCount ? assets[index + columnCount] : {}

  const keydownHandler = ({ code }) => {
    if (!selectedId) return

    if (code === 'Escape') {
      setIsVisible(false)
    }

    if (code === 'Space') {
      setIsVisible((currentIsVisible) => !currentIsVisible)
    }

    if (code === 'ArrowLeft' && previousId) {
      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, id: previousId, query },
        },
        `/${projectId}/visualizer${formatUrl({ id: previousId, query })}`,
      )
    }

    if (code === 'ArrowRight' && nextId) {
      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, id: nextId, query },
        },
        `/${projectId}/visualizer${formatUrl({ id: nextId, query })}`,
      )
    }

    if (code === 'ArrowUp' && previousRowId) {
      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, id: previousRowId, query },
        },
        `/${projectId}/visualizer${formatUrl({ id: previousRowId, query })}`,
      )
    }

    if (code === 'ArrowDown' && nextRowId) {
      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, id: nextRowId, query },
        },
        `/${projectId}/visualizer${formatUrl({ id: nextRowId, query })}`,
      )
    }
  }

  useEffect(() => {
    document.addEventListener('keydown', keydownHandler)

    return () => document.removeEventListener('keydown', keydownHandler)
  })

  if (isVisible && projectId && selectedId) {
    return (
      <div
        css={{
          position: 'absolute',
          top: 0,
          bottom: 0,
          left: 0,
          right: 0,
          zIndex: 999,
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
          <CrossSvg width={20} />
        </Button>

        <div
          css={{
            width: '100%',
            height: '100%',
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
              key={selectedId}
              projectId={projectId}
              assetId={selectedId}
            />
          </SuspenseBoundary>
        </div>
      </div>
    )
  }

  return null
}

AssetsLightbox.propTypes = {
  assets: PropTypes.arrayOf(PropTypes.object).isRequired,
  columnCount: PropTypes.number.isRequired,
}

export default AssetsLightbox
