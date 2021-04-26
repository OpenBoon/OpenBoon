import PropTypes from 'prop-types'

import { colors, spacing, typography, constants } from '../Styles'

import ImagesSvg from '../Icons/images.svg'
import DocumentsSvg from '../Icons/documents.svg'
import VideosSvg from '../Icons/videos.svg'

import { formatUsage } from './helpers'

const LARGE = 400
const SMALL = 300

const ProjectMetrics = ({
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
}) => {
  return (
    <div css={{ display: 'flex', flexWrap: 'wrap', gap: spacing.spacious }}>
      <div css={{ flex: 2 }}>
        <h4>Usage</h4>

        <table
          css={{
            minWidth: LARGE,
            th: {
              textAlign: 'left',
              verticalAlign: 'bottom',
              color: colors.structure.zinc,
              fontFamily: typography.family.condensed,
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
              fontWeight: typography.weight.regular,
              padding: spacing.base,
              paddingLeft: 0,
            },
            td: {
              padding: spacing.normal,
              paddingLeft: 0,
              borderBottom: constants.borders.regular.iron,
            },
          }}
        >
          <thead>
            <tr>
              <th>File Type</th>
              <th>
                Internal Modules
                <br />
                ML Usage
              </th>
              <th>
                External Modules
                <br />
                ML Usage
              </th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td
                css={{
                  whiteSpace: 'nowrap',
                  display: 'flex',
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
              </td>
              <td css={{ fontWeight: typography.weight.medium }}>
                {formatUsage({ number: internalImageCount })}
              </td>
              <td css={{ fontWeight: typography.weight.medium }}>
                {formatUsage({ number: externalImageCount })}
              </td>
            </tr>
            <tr>
              <td>
                <div
                  css={{
                    color: colors.graph.iris,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <VideosSvg
                    height={constants.icons.comfy}
                    css={{ paddingRight: spacing.base }}
                  />
                  Video Hours
                </div>
              </td>
              <td css={{ fontWeight: typography.weight.medium }}>
                {formatUsage({ number: internalVideoMinutes / 60 })}
              </td>
              <td css={{ fontWeight: typography.weight.medium }}>
                {formatUsage({ number: externalVideoMinutes / 60 })}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div css={{ flex: 1 }}>
        <h4>Total Asset Storage</h4>

        <table
          css={{
            minWidth: SMALL,
            th: {
              textAlign: 'left',
              verticalAlign: 'bottom',
              color: colors.structure.zinc,
              fontFamily: typography.family.condensed,
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
              fontWeight: typography.weight.regular,
              padding: spacing.base,
              paddingLeft: 0,
            },
            td: {
              padding: spacing.normal,
              paddingLeft: 0,
              borderBottom: constants.borders.regular.iron,
            },
          }}
        >
          <thead>
            <tr>
              <th>
                <br />
                File Type
              </th>
              <th>Total </th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td
                css={{
                  whiteSpace: 'nowrap',
                  display: 'flex',
                }}
              >
                <div
                  css={{
                    display: 'flex',
                    alignItems: 'center',
                    paddingRight: spacing.small,
                    color: colors.signal.canary.base,
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
              </td>
              <td css={{ fontWeight: typography.weight.medium }}>
                {formatUsage({ number: totalImageCount })}
              </td>
            </tr>
            <tr>
              <td>
                <div
                  css={{
                    color: colors.graph.iris,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <VideosSvg
                    height={constants.icons.comfy}
                    css={{ paddingRight: spacing.base }}
                  />
                  Video Hours
                </div>
              </td>
              <td css={{ fontWeight: typography.weight.medium }}>
                {formatUsage({ number: totalVideoMinutes / 60 })}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div
        css={{
          fontSize: typography.size.small,
          lineHeight: typography.height.small,
          color: colors.structure.zinc,
          paddingBottom: spacing.base,
          fontFamily: typography.family.condensed,
        }}
      >
        *pages are processed &amp; counted as individual assets
        {[internalImageCount, externalImageCount, totalImageCount].includes(
          -1,
        ) && (
          <>
            <br />
            **usage is being calculated and is currently unavailable
          </>
        )}
      </div>
    </div>
  )
}

ProjectMetrics.propTypes = {
  mlUsageThisMonth: PropTypes.shape({
    tier1: PropTypes.shape({
      imageCount: PropTypes.number.isRequired,
      videoMinutes: PropTypes.number.isRequired,
    }).isRequired,
    tier2: PropTypes.shape({
      imageCount: PropTypes.number.isRequired,
      videoMinutes: PropTypes.number.isRequired,
    }).isRequired,
  }).isRequired,
  totalStorageUsage: PropTypes.shape({
    imageCount: PropTypes.number.isRequired,
    videoMinutes: PropTypes.number.isRequired,
  }).isRequired,
}

export default ProjectMetrics
