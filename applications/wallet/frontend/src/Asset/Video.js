/* eslint-disable jsx-a11y/media-has-caption */
import { useRef } from 'react'
import PropTypes from 'prop-types'

import MetadataCues from '../MetadataCues'
import Timeline from '../Timeline'

const AssetVideo = ({
  assetRef,
  uri,
  tracks,
  mediaType,
  length,
  isQuickView,
}) => {
  const videoRef = useRef()

  return (
    <div css={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <div css={{ flex: 1, display: 'flex', flexDirection: 'row', height: 0 }}>
        <div css={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          <div
            css={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              height: 0,
            }}
          >
            <video
              ref={videoRef}
              crossOrigin="anonymous"
              css={{ flex: 1, width: '100%', height: 0 }}
              // eslint-disable-next-line react/jsx-props-no-spreading
              {...(isQuickView ? { autoPlay: true } : {})}
              controls
              controlsList="nodownload"
              disablePictureInPicture
            >
              <source ref={assetRef} src={uri} type={mediaType} />

              {tracks.map(({ label, kind, src }) => {
                return (
                  <track
                    key={label}
                    kind={kind}
                    label={label}
                    src={src}
                    default={kind === 'metadata'}
                  />
                )
              })}
            </video>
          </div>
        </div>

        {!isQuickView && <MetadataCues videoRef={videoRef} />}
      </div>

      {!isQuickView && <Timeline videoRef={videoRef} length={length} />}
    </div>
  )
}

AssetVideo.propTypes = {
  assetRef: PropTypes.shape({}).isRequired,
  uri: PropTypes.string.isRequired,
  tracks: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string.isRequired,
      kind: PropTypes.string.isRequired,
      src: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
  mediaType: PropTypes.string.isRequired,
  length: PropTypes.number.isRequired,
  isQuickView: PropTypes.bool.isRequired,
}

export default AssetVideo
