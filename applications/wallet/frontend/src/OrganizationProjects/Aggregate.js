import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import ImagesSvg from '../Icons/images.svg'
import DocumentsSvg from '../Icons/documents.svg'
import VideosSvg from '../Icons/videos.svg'

import { formatUsage } from '../Project/helpers'

const MIN_WIDTH = 500
const MAX_WIDTH = 700

const OrganizationProjectsAggregate = () => {
  const {
    query: { organizationId },
  } = useRouter()

  const {
    data: { results: projects },
  } = useSWR(`/api/v1/organizations/${organizationId}/projects/`)

  const metrics = projects.reduce(
    (
      acc,
      {
        mlUsageThisMonth: {
          tier1: {
            imageCount: internalImageCount,
            videoMinutes: internalVideoMinutes,
          },
          tier2: {
            imageCount: externalImageCount,
            videoMinutes: externalVideoMinutes,
          },
        },
        totalStorageUsage: {
          imageCount: totalImageCount,
          videoMinutes: totalVideoMinutes,
        },
      },
    ) => {
      return {
        internalImageCount: acc.internalImageCount + internalImageCount,
        internalVideoMinutes: acc.internalVideoMinutes + internalVideoMinutes,
        externalImageCount: acc.externalImageCount + externalImageCount,
        externalVideoMinutes: acc.externalVideoMinutes + externalVideoMinutes,
        totalImageCount: acc.totalImageCount + totalImageCount,
        totalVideoMinutes: acc.totalVideoMinutes + totalVideoMinutes,
      }
    },
    {
      internalImageCount: 0,
      internalVideoMinutes: 0,
      externalImageCount: 0,
      externalVideoMinutes: 0,
      totalImageCount: 0,
      totalVideoMinutes: 0,
    },
  )

  return (
    <div css={{ display: 'flex', flexWrap: 'wrap' }}>
      <div
        css={{
          flex: 1,
          backgroundColor: colors.structure.iron,
          borderRadius: constants.borderRadius.medium,
          padding: spacing.comfy,
          marginRight: spacing.comfy,
          boxShadow: constants.boxShadows.tableRow,
          minWidth: MIN_WIDTH,
          maxWidth: MAX_WIDTH,
          marginBottom: spacing.normal,
        }}
      >
        <div
          css={{
            display: 'flex',
            paddingBottom: spacing.normal,
            borderBottom: constants.borders.regular.mattGrey,
          }}
        >
          <div
            css={{
              display: 'flex',
              alignItems: 'center',
              color: colors.signal.canary.base,
              paddingRight: spacing.small,
            }}
          >
            <ImagesSvg
              height={constants.icons.comfy}
              css={{
                paddingRight: spacing.base,
              }}
            />
            Images
          </div>
          <div
            css={{
              display: 'flex',
              alignItems: 'center',
              color: colors.structure.steel,
              fontSize: typography.size.small,
            }}
          >
            /
          </div>
          <div
            css={{
              display: 'flex',
              alignItems: 'center',
              color: colors.graph.seafoam,
            }}
          >
            <DocumentsSvg
              height={constants.icons.comfy}
              css={{
                paddingLeft: spacing.small,
                paddingRight: spacing.base,
              }}
            />
            Documents*
          </div>
        </div>

        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            marginTop: spacing.normal,
          }}
        >
          <div
            css={{
              flex: 1,
              color: colors.structure.pebble,
            }}
          >
            Internal Module
            <br />
            ML Usage
            <div
              css={{
                fontWeight: typography.weight.bold,
                fontSize: typography.size.giant,
                paddingTop: spacing.moderate,
                color: colors.structure.white,
              }}
            >
              {formatUsage({ number: metrics.internalImageCount })}
            </div>
          </div>

          <div
            css={{
              flex: 1,
              color: colors.structure.pebble,
              borderLeft: constants.borders.regular.mattGrey,
              paddingLeft: spacing.normal,
            }}
          >
            External Module
            <br />
            ML Usage
            <div
              css={{
                fontWeight: typography.weight.bold,
                fontSize: typography.size.giant,
                paddingTop: spacing.moderate,
                color: colors.structure.white,
              }}
            >
              {formatUsage({ number: metrics.externalImageCount })}
            </div>
          </div>

          <div
            css={{
              flex: 1,
              color: colors.structure.pebble,
              borderLeft: constants.borders.regular.mattGrey,
              paddingLeft: spacing.normal,
            }}
          >
            Total Assets
            <br />
            Stored
            <div
              css={{
                fontWeight: typography.weight.bold,
                fontSize: typography.size.giant,
                paddingTop: spacing.moderate,
                color: colors.structure.white,
              }}
            >
              {formatUsage({ number: metrics.totalImageCount })}
            </div>
          </div>
        </div>
      </div>

      <div
        css={{
          flex: 1,
          backgroundColor: colors.structure.iron,
          borderRadius: constants.borderRadius.medium,
          padding: spacing.comfy,
          boxShadow: constants.boxShadows.tableRow,
          minWidth: MIN_WIDTH,
          maxWidth: MAX_WIDTH,
          marginBottom: spacing.normal,
        }}
      >
        <div
          css={{
            color: colors.graph.iris,
            display: 'flex',
            alignItems: 'center',
            paddingBottom: spacing.normal,
            borderBottom: constants.borders.regular.mattGrey,
          }}
        >
          <VideosSvg
            height={constants.icons.comfy}
            css={{ paddingRight: spacing.base }}
          />
          Video Hours
        </div>

        <div css={{ display: 'flex', marginTop: spacing.normal }}>
          <div
            css={{
              flex: 1,
              color: colors.structure.pebble,
            }}
          >
            Internal Module
            <br />
            ML Usage
            <div
              css={{
                fontWeight: typography.weight.bold,
                fontSize: typography.size.giant,
                paddingTop: spacing.moderate,
                color: colors.structure.white,
              }}
            >
              {formatUsage({ number: metrics.internalVideoMinutes / 60 })}
            </div>
          </div>

          <div
            css={{
              flex: 1,
              color: colors.structure.pebble,
              borderLeft: constants.borders.regular.mattGrey,
              paddingLeft: spacing.normal,
            }}
          >
            External Module
            <br />
            ML Usage
            <div
              css={{
                fontWeight: typography.weight.bold,
                fontSize: typography.size.giant,
                paddingTop: spacing.moderate,
                color: colors.structure.white,
              }}
            >
              {formatUsage({ number: metrics.internalVideoMinutes / 60 })}
            </div>
          </div>

          <div
            css={{
              flex: 1,
              color: colors.structure.pebble,
              borderLeft: constants.borders.regular.mattGrey,
              paddingLeft: spacing.normal,
            }}
          >
            Total Video Hours
            <br />
            Stored
            <div
              css={{
                fontWeight: typography.weight.bold,
                fontSize: typography.size.giant,
                paddingTop: spacing.moderate,
                color: colors.structure.white,
              }}
            >
              {formatUsage({ number: metrics.totalVideoMinutes / 60 })}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default OrganizationProjectsAggregate
