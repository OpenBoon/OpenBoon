import { colors, spacing, typography, constants } from '../Styles'

import ImagesSvg from '../Icons/images.svg'
import DocumentsSvg from '../Icons/documents.svg'
import VideosSvg from '../Icons/videos.svg'

const LARGE = 400
const SMALL = 300

const ProjectMetrics = () => {
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
                --
              </td>
              <td>
                <br />
                <br />
                --
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
              <td>--</td>
              <td>--</td>
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
                --
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
              <td>--</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default ProjectMetrics
