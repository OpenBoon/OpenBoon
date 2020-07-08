import useSWR from 'swr'
import { useRouter } from 'next/router'

import { spacing, constants, typography } from '../Styles'

import Card, { VARIANTS as CARD_VARIANTS } from '../Card'
import { capitalizeFirstLetter } from '../Text/helpers'

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
      variant={CARD_VARIANTS.LIGHT}
      header={`Project Tier: ${capitalizeFirstLetter({ word: tier })}`}
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
              borderBottom: constants.borders.regular.iron,
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
