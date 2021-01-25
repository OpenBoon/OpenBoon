import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, spacing, typography, constants } from '../Styles'

import ImagesSvg from '../Icons/images.svg'
import DocumentsSvg from '../Icons/documents.svg'
import VideosSvg from '../Icons/videos.svg'
import { formatUsage } from './helpers'

const LARGE = 400
const SMALL = 300

const ProjectMetrics = ({ projectId }) => {
  const {
    data: {
      tier_1: {
        image_count: internalImageCount = -1,
        video_minutes: internalVideoMinutes = -1,
      } = {},
      tier_2: {
        image_count: externalImageCount = -1,
        video_minutes: externalVideoMinutes = -1,
      } = {},
    } = {},
  } = useSWR(`/api/v1/projects/${projectId}/ml_usage_this_month/`)

  const {
    data: {
      image_count: totalImageCount = -1,
      video_hours: totalVideoHours = -1,
    } = {},
  } = useSWR(`/api/v1/projects/${projectId}/total_storage_usage/`)

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
                ML Usage
                <br />
                External Modules
              </th>
              <th>
                ML Usage
                <br />
                Internal Modules
              </th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>
                <div
                  css={{
                    color: colors.signal.canary.base,
                    display: 'flex',
                    alignItems: 'center',
                    paddingBottom: spacing.base,
                  }}
                >
                  <ImagesSvg
                    height={constants.icons.comfy}
                    css={{ paddingRight: spacing.base }}
                  />
                  Images /
                </div>
                <div
                  css={{
                    color: colors.graph.seafoam,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <DocumentsSvg
                    height={constants.icons.comfy}
                    css={{ paddingRight: spacing.base }}
                  />
                  Documents*
                </div>
              </td>
              <td>
                <br />
                <br />
                {formatUsage({ number: internalImageCount })}
              </td>
              <td>
                <br />
                <br />
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
              <td>{formatUsage({ number: internalVideoMinutes / 60 })}</td>
              <td>{formatUsage({ number: externalVideoMinutes / 60 })}</td>
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
              <td>
                <div
                  css={{
                    color: colors.signal.canary.base,
                    display: 'flex',
                    alignItems: 'center',
                    paddingBottom: spacing.base,
                  }}
                >
                  <ImagesSvg
                    height={constants.icons.comfy}
                    css={{ paddingRight: spacing.base }}
                  />
                  Images /
                </div>
                <div
                  css={{
                    color: colors.graph.seafoam,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <DocumentsSvg
                    height={constants.icons.comfy}
                    css={{ paddingRight: spacing.base }}
                  />
                  Documents*
                </div>
              </td>
              <td>
                <br />
                <br />
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
              <td>{formatUsage({ number: totalVideoHours })}</td>
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
  projectId: PropTypes.string.isRequired,
}

export default ProjectMetrics
