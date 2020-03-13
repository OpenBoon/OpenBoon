import useSWR from 'swr'
import { useRouter } from 'next/router'

import { colors, spacing, constants } from '../Styles'

import Card from '../Card'

import ProjectUsageBar from './UsageBar'

const IMG_WIDTH = 32

const ProjectUsagePlan = () => {
  const {
    query: { projectId },
  } = useRouter()

  const {
    data: { results: subscriptions },
  } = useSWR(`/api/v1/projects/${projectId}/subscriptions/`)

  if (subscriptions.length === 0) return null

  const {
    limits: { videoHours: videoLimit, imageCount: imageLimit },
    usage: { videoHours: videoUsage, imageCount: imageUsage },
  } = subscriptions[0]

  return (
    <Card title="Project Usage Plan">
      <div
        css={{
          borderBottom: constants.borders.tabs,
          paddingBottom: spacing.normal,
        }}>
        <h4
          css={{
            display: 'flex',
            alignItems: 'center',
            img: {
              marginRight: spacing.moderate,
            },
          }}>
          <img src="/icons/videos.png" alt="" width={IMG_WIDTH} /> Video:{' '}
          {videoLimit.toLocaleString()} hours
        </h4>
        <ProjectUsageBar limit={videoLimit} usage={videoUsage} legend="/hrs" />
      </div>
      <div
        css={{
          borderBottom: constants.borders.tabs,
          paddingTop: spacing.normal,
          paddingBottom: spacing.normal,
        }}>
        <h4
          css={{
            display: 'flex',
            alignItems: 'center',
            img: {
              marginRight: spacing.moderate,
            },
          }}>
          <img src="/icons/images.png" alt="" width={IMG_WIDTH} /> Image /
          Documents: {imageLimit.toLocaleString()}
        </h4>
        <ProjectUsageBar limit={imageLimit} usage={imageUsage} legend="" />
      </div>
      <div
        css={{
          borderBottom: constants.borders.tabs,
          paddingTop: spacing.normal,
          paddingBottom: spacing.normal,
        }}>
        <h4>Additional Modules:</h4>
        <span css={{ color: colors.structure.zinc, fontStyle: 'italic' }}>
          None
        </span>
      </div>
      <div css={{ paddingTop: spacing.normal, color: colors.structure.zinc }}>
        Contact your Account Manager to add additional modules and resources to
        your plan.
      </div>
    </Card>
  )
}

export default ProjectUsagePlan
