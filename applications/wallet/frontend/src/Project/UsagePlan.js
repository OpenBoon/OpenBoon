import useSWR from 'swr'
import { useRouter } from 'next/router'

import { spacing, constants, typography } from '../Styles'

import Card from '../Card'

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
    tier,
    usage: { videoHours: videoUsage, imageCount: imageUsage },
  } = subscriptions[0]

  return (
    <Card
      header={`Project Tier: ${
        tier.charAt(0).toUpperCase() + tier.substring(1)
      }`}
      content={
        <>
          <h3
            css={{
              paddingBottom: spacing.comfy,
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
            }}
          >
            Usage:
          </h3>
          <div
            css={{
              borderBottom: constants.borders.tabs,
              paddingBottom: spacing.normal,
            }}
          >
            <h4
              css={{
                display: 'flex',
                alignItems: 'center',
                img: {
                  marginRight: spacing.moderate,
                },
              }}
            >
              <img src="/icons/videos.png" alt="" width={IMG_WIDTH} /> Video
              Hours: {videoUsage.toLocaleString()}
            </h4>
          </div>
          <div
            css={{
              paddingTop: spacing.normal,
              paddingBottom: spacing.normal,
            }}
          >
            <h4
              css={{
                display: 'flex',
                alignItems: 'center',
                img: {
                  marginRight: spacing.moderate,
                },
              }}
            >
              <img src="/icons/images.png" alt="" width={IMG_WIDTH} /> Image /
              Documents: {imageUsage.toLocaleString()}
            </h4>
          </div>
        </>
      }
    />
  )
}

export default ProjectUsagePlan
