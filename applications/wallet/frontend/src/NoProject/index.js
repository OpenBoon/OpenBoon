import Head from 'next/head'

import NoProjectSvg from '../Icons/noProject.svg'

import { colors } from '../Styles'

const NoProject = () => {
  return (
    <div>
      <Head>
        <title>Zorroa</title>
      </Head>
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}>
        <NoProjectSvg width={250} />
        <h3 css={{ color: colors.structure.steel }}>
          You currently have no projects.
        </h3>
      </div>
    </div>
  )
}

export default NoProject
