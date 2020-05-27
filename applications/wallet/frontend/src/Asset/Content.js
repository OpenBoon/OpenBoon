/* eslint-disable jsx-a11y/media-has-caption */
import Link from 'next/link'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { constants, colors, spacing, typography, zIndex } from '../Styles'

import Panel from '../Panel'
import Metadata from '../Metadata'

import InformationSvg from '../Icons/information.svg'
import CrossSvg from '../Icons/cross.svg'

import Button, { VARIANTS } from '../Button'

const ICON_WIDTH = 20

const AssetContent = () => {
  const {
    query: { projectId, id: assetId, query },
  } = useRouter()

  const {
    data: {
      metadata: {
        files,
        media: { type },
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  const srcFile =
    type === 'video'
      ? files.find(({ mimetype }) => {
          return mimetype.includes('video')
        })
      : files.reduce((acc, file) => {
          if (!acc || file.size > acc.size) {
            return file
          }

          return acc
        }, '')

  const {
    name,
    attrs: { width, height },
  } = srcFile

  const queryString = query ? `?query=${query}` : ''

  const videoStyle =
    width > height
      ? { height: '100%', maxWidth: '100%' }
      : { width: '100%', maxHeight: '100%' }

  const largerDimension = width > height ? 'width' : 'height'
  const fileSrc = `/api/v1/projects/${projectId}/assets/${assetId}/files/category/proxy/name/${name}/`

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
          href={`/[projectId]/visualizer${queryString}`}
          as={`/${projectId}/visualizer${queryString}`}
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
                }}
              >
                CLOSE VIEW
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
          {type === 'video' && (
            <video
              css={videoStyle}
              autoPlay
              controls
              controlsList="nodownload"
              disablePictureInPicture
            >
              <source src={fileSrc} type="video/mp4" />
            </video>
          )}
          {type !== 'video' && (
            <img css={{ [largerDimension]: '100%' }} src={fileSrc} alt={name} />
          )}
        </div>
        <Panel openToThe="left">
          {{
            metadata: {
              title: 'Asset Metadata',
              icon: <InformationSvg width={ICON_WIDTH} aria-hidden />,
              content: <Metadata />,
            },
          }}
        </Panel>
      </div>
    </div>
  )
}

export default AssetContent
