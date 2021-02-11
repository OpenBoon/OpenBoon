import { useState, useEffect } from 'react'
import PropTypes from 'prop-types'

import { spacing, constants, colors } from '../Styles'

import CaptionsSvg from '../Icons/captions.svg'
import GearSvg from '../Icons/gear.svg'

import Button, { VARIANTS } from '../Button'
import Menu from '../Menu'

const SEPARATOR_WIDTH = 2

let lastEnabledTrackIndex = -1

const TimelineCaptions = ({ videoRef, initialTrackIndex }) => {
  const [trackIndex, setTrackIndex] = useState(initialTrackIndex)

  const textTracks = videoRef.current?.textTracks

  useEffect(() => {
    /* istanbul ignore next */
    if (!textTracks) return () => {}

    if (lastEnabledTrackIndex === -1) {
      lastEnabledTrackIndex = Object.values(textTracks).findIndex(
        ({ kind }) => kind === 'captions',
      )
    }

    /* istanbul ignore next */
    const onChange = () => {
      setTrackIndex(-1)

      for (let i = 0; i < textTracks.length; i += 1) {
        if (textTracks[i].mode === 'showing') {
          setTrackIndex(i)
        }
      }
    }

    textTracks.addEventListener('change', onChange)

    return () => textTracks.removeEventListener('change', onChange)
  }, [textTracks])

  if (!textTracks || !textTracks[0]) return null

  const captionTracks = Object.values(textTracks).filter(
    ({ kind }) => kind === 'captions',
  )

  if (captionTracks.length === 0) return null

  return (
    <div css={{ display: 'flex' }}>
      <Button
        aria-label={`${trackIndex > -1 ? 'Disable' : 'Enable'} Captions`}
        variant={VARIANTS.ICON}
        style={{
          padding: spacing.small,
          color: trackIndex > -1 ? colors.key.one : colors.structure.steel,
          ':hover, &.focus-visible:focus': {
            color: trackIndex > -1 ? colors.key.one : colors.structure.white,
            backgroundColor: colors.structure.mattGrey,
          },
        }}
        onClick={() => {
          const index = trackIndex > -1 ? trackIndex : lastEnabledTrackIndex

          // eslint-disable-next-line no-param-reassign
          textTracks[index].mode =
            textTracks[index]?.mode === 'showing' ? 'disabled' : 'showing'
        }}
      >
        <CaptionsSvg height={constants.icons.regular} />
      </Button>

      <div css={{ width: spacing.small }} />

      <Menu
        open="top-left"
        style={{ bottom: '100%' }}
        button={({ onBlur, onClick }) => {
          return (
            <Button
              aria-label="Toggle Captions Menu"
              className="actions"
              variant={VARIANTS.ICON}
              style={{
                padding: spacing.small,
                ':hover, &.focus-visible:focus': {
                  backgroundColor: colors.structure.mattGrey,
                },
              }}
              onBlur={onBlur}
              onClick={onClick}
            >
              <GearSvg height={constants.icons.regular} />
            </Button>
          )
        }}
      >
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              {captionTracks.map(({ label }) => {
                const index = Object.values(textTracks).findIndex(
                  ({ label: l }) => l === label,
                )
                return (
                  <li key={label}>
                    <Button
                      variant={VARIANTS.MENU_ITEM}
                      css={
                        trackIndex === index
                          ? {
                              color: colors.key.white,
                              backgroundColor: colors.structure.mattGrey,
                            }
                          : {}
                      }
                      onBlur={onBlur}
                      onClick={(event) => {
                        for (let i = 0; i < textTracks.length; i += 1) {
                          textTracks[i].mode = 'disabled'
                        }

                        textTracks[index].mode = 'showing'

                        lastEnabledTrackIndex = index

                        onClick(event)

                        onBlur(event)
                      }}
                    >
                      {label}
                    </Button>
                  </li>
                )
              })}
            </ul>
          </div>
        )}
      </Menu>

      <div
        css={{
          width: SEPARATOR_WIDTH,
          backgroundColor: colors.structure.coal,
          margin: spacing.small,
        }}
      />
    </div>
  )
}

TimelineCaptions.propTypes = {
  videoRef: PropTypes.shape({
    current: PropTypes.shape({
      textTracks: PropTypes.shape({
        addEventListener: PropTypes.func,
        removeEventListener: PropTypes.func,
      }).isRequired,
    }),
  }).isRequired,
  initialTrackIndex: PropTypes.number.isRequired,
}

export default TimelineCaptions
