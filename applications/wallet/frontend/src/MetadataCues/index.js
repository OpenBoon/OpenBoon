import { useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import AutoSizer from 'react-virtualized-auto-sizer'
import { useRouter } from 'next/router'

import { constants } from '../Styles'

import Resizeable from '../Resizeable'

import { getMetadata } from './helpers'

import MetadataCuesContent from './Content'

export const MIN_WIDTH = 400

export const noop = () => {}

const MetadataCues = ({ videoRef }) => {
  const {
    query: { assetId },
  } = useRouter()

  const [metadata, setMetadata] = useState({})

  /* istanbul ignore next */
  useEffect(() => {
    const video = videoRef.current
    const textTracks = video?.textTracks || {}

    const metadataTracks = Object.values(textTracks).filter(
      ({ kind }) => kind === 'metadata',
    )

    if (!metadataTracks) return () => {}

    const onCueChange = (event) => {
      const newMetadata = getMetadata(event)

      setMetadata((m) => ({ ...m, ...newMetadata }))
    }

    metadataTracks.forEach((track) =>
      track.addEventListener('cuechange', onCueChange),
    )

    return () =>
      metadataTracks.forEach((track) =>
        track.removeEventListener('cuechange', onCueChange),
      )
  }, [videoRef])

  return (
    <AutoSizer defaultHeight={0} disableWidth>
      {
        /* istanbul ignore next */
        ({ height }) => (
          <div
            css={{
              height,
              borderLeft: constants.borders.regular.black,
              borderBottom: constants.borders.regular.black,
            }}
          >
            <Resizeable
              storageName={`MetadataCues.${assetId}`}
              minSize={MIN_WIDTH}
              openToThe="left"
              isInitiallyOpen
            >
              <MetadataCuesContent metadata={metadata} />
            </Resizeable>
          </div>
        )
      }
    </AutoSizer>
  )
}

MetadataCues.propTypes = {
  videoRef: PropTypes.shape({
    current: PropTypes.shape({
      duration: PropTypes.number,
      textTracks: PropTypes.shape({
        addEventListener: PropTypes.func,
        removeEventListener: PropTypes.func,
      }).isRequired,
    }),
  }).isRequired,
}

export default MetadataCues
