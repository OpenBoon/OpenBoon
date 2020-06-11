/* eslint-disable jsx-a11y/media-has-caption */
import { useRef, useEffect, useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

const FALLBACK_IMG = '/icons/fallback_3x.png'

const AssetAsset = ({ projectId, assetId }) => {
  const [hasError, setHasError] = useState(false)

  const assetRef = useRef()

  /* istanbul ignore next */
  useEffect(() => {
    const asset = assetRef.current

    if (!asset) return () => {}

    const fallback = () => {
      setHasError(true)
    }

    asset.addEventListener('error', fallback)

    return () => asset.removeEventListener('error', fallback)
  })

  const {
    data: {
      metadata: {
        source: { filename },
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  const {
    data: { mediaType, uri },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/signed_url/`)

  const isVideo = mediaType.includes('video')

  return (
    <div
      css={{
        flex: 1,
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
      }}
    >
      {isVideo && !hasError ? (
        <video
          css={{ width: '100%', height: '100%', objectFit: 'contain' }}
          autoPlay
          controls
          controlsList="nodownload"
          disablePictureInPicture
        >
          <source ref={assetRef} src={uri} type={mediaType} />
        </video>
      ) : (
        <img
          ref={assetRef}
          css={{ width: '100%', height: '100%', objectFit: 'contain' }}
          src={hasError ? /* istanbul ignore next */ FALLBACK_IMG : uri}
          alt={filename}
        />
      )}
    </div>
  )
}

AssetAsset.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default AssetAsset
