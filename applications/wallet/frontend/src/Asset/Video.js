/* eslint-disable jsx-a11y/media-has-caption */
import { useRef } from 'react'
import PropTypes from 'prop-types'

import Feature from '../Feature'
import Timeline from '../Timeline'

const AssetVideo = ({ assetRef, uri, mediaType }) => {
  const videoRef = useRef()

  return (
    <div css={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <video
        ref={videoRef}
        css={{ flex: 1, width: '100%', height: 0 }}
        autoPlay
        controls
        controlsList="nodownload"
        disablePictureInPicture
      >
        <source ref={assetRef} src={uri} type={mediaType} />
      </video>

      <Feature flag="timeline" envs={[]}>
        <Timeline videoRef={videoRef} />
      </Feature>
    </div>
  )
}

AssetVideo.propTypes = {
  assetRef: PropTypes.shape({}).isRequired,
  uri: PropTypes.string.isRequired,
  mediaType: PropTypes.string.isRequired,
}

export default AssetVideo
