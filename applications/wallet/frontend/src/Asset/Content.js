/* eslint-disable jsx-a11y/media-has-caption */
import Link from 'next/link'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { constants, colors, spacing, typography, zIndex } from '../Styles'

import Panel from '../Panel'
import Metadata from '../Metadata'
import AssetDelete from '../AssetDelete'

import InformationSvg from '../Icons/information.svg'
import CrossSvg from '../Icons/cross.svg'
import TrashSvg from '../Icons/trash.svg'

import Button, { VARIANTS } from '../Button'

const ICON_WIDTH = 20

const AssetContent = () => {
  const {
    query: { projectId, id: assetId, query },
  } = useRouter()

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

  const idString = `?id=${assetId}`
  const queryString = query ? `&query=${query}` : ''

  return (
    <div
      css={{
        height: '100%',
        backgroundColor: colors.structure.coal,
        marginLeft: -spacing.spacious,
        marginRight: -spacing.spacious,
        marginBottom: -spacing.spacious,
        paddingTop: spacing.hairline,
        display: 'flex',
        flex: 1,
        flexDirection: 'column',
      }}
    >
      <div
        css={{
          display: 'flex',
          height: '100%',
          overflowY: 'hidden',
          position: 'relative',
        }}
      >
        <Link
          href={`/[projectId]/visualizer${idString}${queryString}`}
          as={`/${projectId}/visualizer${idString}${queryString}`}
          passHref
        >
          <Button
            variant={VARIANTS.MENU_ITEM}
            css={{
              position: 'absolute',
              top: spacing.base,
              left: spacing.base,
              padding: spacing.base,
              color: colors.structure.steel,
              borderRadius: constants.borderRadius.small,
              ':hover': {
                color: colors.structure.white,
              },
              zIndex: zIndex.layout.interactive,
            }}
          >
            <div css={{ display: 'flex' }}>
              <CrossSvg width={20} />
              <span
                css={{
                  paddingLeft: spacing.base,
                  fontSize: typography.size.medium,
                  lineHeight: typography.height.medium,
                  fontWeight: typography.weight.bold,
                  textTransform: 'uppercase',
                }}
              >
                Close View
              </span>
            </div>
          </Button>
        </Link>
        <div
          css={{
            flex: 1,
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
          }}
        >
          {isVideo ? (
            <video
              css={{ width: '100%', height: '100%', objectFit: 'contain' }}
              autoPlay
              controls
              controlsList="nodownload"
              disablePictureInPicture
            >
              <source src={uri} type="video/mp4" />
            </video>
          ) : (
            <img
              css={{ width: '100%', height: '100%', objectFit: 'contain' }}
              src={uri}
              alt={filename}
            />
          )}
        </div>
        <Panel openToThe="left">
          {{
            metadata: {
              title: 'Asset Metadata',
              icon: <InformationSvg width={ICON_WIDTH} aria-hidden />,
              content: <Metadata />,
            },
            delete: {
              title: 'Delete',
              icon: <TrashSvg width={ICON_WIDTH} aria-hidden />,
              content: <AssetDelete />,
            },
          }}
        </Panel>
      </div>
    </div>
  )
}

export default AssetContent
