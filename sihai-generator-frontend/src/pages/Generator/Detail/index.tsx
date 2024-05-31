import {PageContainer,} from '@ant-design/pro-components';
import {Button, Card, Col, Image, message, Row, Space, Tabs, Tag, Typography} from 'antd';
import React, {useEffect, useState} from 'react';
import {downloadUsingGet, getGeneratorVoByIdUsingGet} from "@/services/backend/generatorController";
import {useModel, useParams} from "@@/exports";
import moment from "moment";
import {DownloadOutlined, EditOutlined} from "@ant-design/icons";
import FileConfig from "@/pages/Generator/Detail/components/FileConfig";
import ModelConfig from "@/pages/Generator/Detail/components/ModelConfig";
import AuthorInfo from "@/pages/Generator/Detail/components/AuthorInfo";
import {COS_HOST} from "@/constants";
import {saveAs} from "file-saver";
import initialState from "@@/plugin-initialState/@@initialState";
import {Link} from "umi";

/**
 * 生成器详情页
 * @constructor
 */
const GeneratorDetailPage: React.FC = () => {

  const {id} = useParams();

  const [loading, setLoading] = useState<boolean>(true);

  const [data, setData] = useState<API.GeneratorVO>({});

  // 获取登陆状态
  const { initialState, setInitialState } = useModel('@@initialState');

  // 获取登录用户
  const { currentUser } = initialState ?? {};

  // 判断是否为当前用户
  const my = data?.userId === currentUser?.id;

  /**
   * 加载数据
   */
  const loadData = async () => {
    if (!id) {
      return;
    }
    setLoading(true);
    try {
      const res = await getGeneratorVoByIdUsingGet({
        id,
      });
      setData(res.data ?? {});
    } catch (error: any) {
      message.error('加载数据失败，' + error.message);
    }
    setLoading(false);
  };

  /**
   * 页面加载
   */
  useEffect(() => {
    if (id) {
      loadData();
    }
  }, [id]);

  /**
   * 标签列表
   */
  const tagListView = (tags?: string[]) => {
    if (!tags) {
      return <></>;
    }
    return (
      <div style={{marginBottom: 8}}>
        {tags.map((tag) => (
          <Tag key={tag}>{tag}</Tag>
        ))}
      </div>
    );
  };

  /**
   * 判断下载按钮显示
   */
  const downloadButton = data.distPath && currentUser && (
    <Button
      icon={<DownloadOutlined/>}
      onClick={async () => {
        const blob = await downloadUsingGet(
          { id },
          {
            responseType: 'blob',
          },
        );
        // 使用 file-saver 来保存文件
        const fullPath = COS_HOST + data.distPath;
        saveAs(blob, fullPath.substring(fullPath.lastIndexOf('/') + 1));
      }}
    >
      下载
    </Button>
  )

  /**
   * 判断编辑按钮显示
   */
  const editButton = my && (
    <Link to={`/generator/update?id=${data.id}`}>
      <Button icon={<EditOutlined />}>编辑</Button>
    </Link>
  )

  return (
    <PageContainer title={<></>} loading={loading}>
      <Card>
        <Row justify={"space-between"} gutter={[32, 32]}>
          {/*左侧--基本信息*/}
          <Col flex={"auto"}>
            <Space size={"large"} align={"center"}>
              <Typography.Title level={4}>{data.name}</Typography.Title>
              {tagListView(data.tags)}
            </Space>
            <Typography.Paragraph>{data.description}</Typography.Paragraph>
            <Typography.Paragraph type={"secondary"}>
              创建时间: {moment(data.createTime).format("YYYY-MM-DD HH:mm:ss")}
            </Typography.Paragraph>
            <Typography.Paragraph type={"secondary"}>基础包: {data.basePackage}</Typography.Paragraph>
            <Typography.Paragraph type={"secondary"}>版本: {data.version}</Typography.Paragraph>
            <Typography.Paragraph type={"secondary"}>作者: {data.author}</Typography.Paragraph>
            <div style={{marginBottom: 24}}/>
            <Space size="middle">
              <Link to={`/generator/use/${data.id}`}>
                <Button type="primary">立即使用</Button>
              </Link>
              {downloadButton}
              {editButton}
            </Space>
          </Col>
          {/*右侧--图片*/}
          <Col flex={"360px"} style={{margin: 15, marginRight: 20}}>
            <Image src={data.picture} style={{width: 360, height: 360}} />
          </Col>
        </Row>
      </Card>
      <div style={{marginBottom: 24}}/>
      <Card>
        <Tabs
          size="large"
          defaultActiveKey={'fileConfig'}
          onChange={() => {
          }}
          items={[
            {
              key: 'fileConfig',
              label: '文件配置',
              children: <FileConfig data={data}/>,
            },
            {
              key: 'modelConfig',
              label: '模型配置',
              children: <ModelConfig data={data}/>,
            },
            {
              key: 'userInfo',
              label: '作者信息',
              children: <AuthorInfo data={data}/>,
            },
          ]}
        />
      </Card>
    </PageContainer>
  );
};

export default GeneratorDetailPage;
