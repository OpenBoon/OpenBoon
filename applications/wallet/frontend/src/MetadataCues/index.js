import { useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import AutoSizer from 'react-virtualized-auto-sizer'

import { constants } from '../Styles'

import { getMetadata } from './helpers'

import MetadataCuesContent from './Content'

// TODO: make resizeable
const MIN_WIDTH = 400

const MetadataCues = ({ videoRef }) => {
  const [metadata, setMetadata] = useState({})

  const video = videoRef.current
  const textTracks = video?.textTracks || {}

  const metadataTracks = Object.values(textTracks).filter(
    ({ kind }) => kind === 'metadata',
  )

  /* istanbul ignore next */
  useEffect(() => {
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
  }, [metadataTracks])

  if (!video || !video.duration || metadataTracks.length === 0) return null

  return (
    <div
      css={{
        width: MIN_WIDTH,
        borderLeft: constants.borders.regular.black,
        borderBottom: constants.borders.regular.black,
      }}
    >
      <AutoSizer defaultHeight={0} disableWidth>
        {
          /* istanbul ignore next */
          ({ height }) => (
            <MetadataCuesContent metadata={metadata} height={height} />
          )
        }
      </AutoSizer>
    </div>
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
