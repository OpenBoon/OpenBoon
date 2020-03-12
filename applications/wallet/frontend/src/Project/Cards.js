import useSWR from 'swr'
import { useRouter } from 'next/router'
import Link from 'next/link'

import Card from '../Card'
import Button, { VARIANTS } from '../Button'

const ProjectCards = () => {
  const {
    query: { projectId },
  } = useRouter()

  const {
    data: { id, name },
  } = useSWR(`/api/v1/projects/${projectId}/`)

  return (
    <div css={{ display: 'flex', flexWrap: 'wrap' }}>
      <Card title="">
        <h3>Project: {name}</h3>
        &nbsp;
        <div>Project ID: {id}</div>
        &nbsp;
        <div css={{ display: 'flex' }}>
          <Link
            href="/[projectId]/users/add"
            as={`/${projectId}/users/add`}
            passHref>
            <Button variant={VARIANTS.PRIMARY_SMALL}>
              + Add Users To Project
            </Button>
          </Link>
        </div>
      </Card>

      <Card title="Getting Started">Step 1: Create a Data Source</Card>

      <Card title="Project API Keys">
        You must have an active project before you can create an API key. You
        will need an API key to XYZ.
      </Card>

      <Card title="Project Usage Plan">Video</Card>
    </div>
  )
}

export default ProjectCards
