import { useState, useEffect } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'

import Button, { VARIANTS } from '../Button'

import CirclePlusSvg from '../Icons/circlePlus.svg'
import CircleMinusSvg from '../Icons/circleMinus.svg'

import { getScroller } from '../Scroll/helpers'

import { getNextScrollLeft } from './helpers'

import { ACTIONS } from './reducer'

const TimelineResize = ({ dispatch, zoom, videoRef, rulerRef }) => {
  const scroller = getScroller({ namespace: 'Timeline' })

  const [nextScrollLeft, setNextScrollLeft] = useState(0)

  // useEffect(() => {
  //   scroller.emit({
  //     eventName: 'scroll',
  //     data: {
  //       scrollX: nextScrollLeft,
  //       scrollY: 0,
  //     },
  //   })
  // }, [nextScrollLeft, scroller])

  return (
    <div
      css={{
        position: 'absolute',
        bottom: spacing.normal,
        right: spacing.normal,
        display: 'flex',
        border: constants.borders.regular.iron,
        borderRadius: constants.borderRadius.small,
        backgroundColor: colors.structure.lead,
        boxShadow: constants.boxShadows.default,
        zIndex: zIndex.timeline.menu,
        opacity: constants.opacity.eighth,
        paddingTop: spacing.small,
        paddingBottom: spacing.small,
        paddingLeft: spacing.base,
        paddingRight: spacing.base,
      }}
    >
      <Button
        aria-label="Zoom Out"
        onClick={() => {
          const nextZoom = zoom - 100
          dispatch({ type: ACTIONS.DECREMENT, payload: { value: nextZoom } })

          const scrollLeft = getNextScrollLeft({
            videoRef,
            rulerRef,
            zoom,
            nextZoom,
          })

          setNextScrollLeft(scrollLeft)
        }}
        isDisabled={zoom === 100}
        variant={VARIANTS.NEUTRAL}
        css={{
          padding: spacing.base,
          ':hover': {
            color: colors.key.one,
          },
          '&[aria-disabled=true]': {
            color: colors.structure.steel,
          },
          opacity: constants.opacity.full,
        }}
      >
        <CircleMinusSvg height={constants.icons.regular} />
      </Button>
      <Button
        aria-label="Zoom In"
        onClick={() => {
          const nextZoom = zoom + 100
          dispatch({ type: ACTIONS.INCREMENT, payload: { value: nextZoom } })

          const scrollLeft = getNextScrollLeft({
            videoRef,
            rulerRef,
            zoom,
            nextZoom,
          })

          setNextScrollLeft(scrollLeft)
        }}
        isDisabled={false}
        variant={VARIANTS.NEUTRAL}
        css={{
          padding: spacing.base,
          ':hover': {
            color: colors.key.one,
          },
          '&[aria-disabled=true]': {
            color: colors.structure.steel,
          },
          opacity: constants.opacity.full,
        }}
      >
        <CirclePlusSvg height={constants.icons.regular} />
      </Button>
    </div>
  )
}

TimelineResize.propTypes = {
  dispatch: PropTypes.func.isRequired,
  zoom: PropTypes.number.isRequired,
  videoRef: PropTypes.shape({
    current: PropTypes.shape({
      currentTime: PropTypes.number,
      duration: PropTypes.number,
    }),
  }).isRequired,
  rulerRef: PropTypes.shape({
    current: PropTypes.shape({
      scrollWidth: PropTypes.number,
      scrollLeft: PropTypes.number,
    }),
  }).isRequired,
}

export default TimelineResize
